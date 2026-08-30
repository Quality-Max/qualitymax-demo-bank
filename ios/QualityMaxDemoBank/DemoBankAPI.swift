import Foundation

enum DemoBankAPIError: LocalizedError, Sendable {
    case invalidBaseURL
    case invalidResponse
    case server(message: String, statusCode: Int)

    var errorDescription: String? {
        switch self {
        case .invalidBaseURL:
            return "The Demo Bank backend URL is invalid."
        case .invalidResponse:
            return "The Demo Bank backend returned an invalid response."
        case let .server(message, _):
            return message
        }
    }

    var statusCode: Int? {
        if case let .server(_, statusCode) = self {
            return statusCode
        }
        return nil
    }
}

actor DemoBankAPI {
    let baseURLString: String

    private let baseURL: URL
    private let session: URLSession
    private let encoder = JSONEncoder()
    private let decoder = JSONDecoder()

    init(baseURLString: String = DemoBankAPI.configuredBaseURL) {
        self.baseURLString = baseURLString.trimmingCharacters(in: CharacterSet(charactersIn: "/"))
        guard let url = URL(string: self.baseURLString) else {
            preconditionFailure(DemoBankAPIError.invalidBaseURL.localizedDescription)
        }
        baseURL = url

        let configuration = URLSessionConfiguration.default
        configuration.httpCookieStorage = .shared
        configuration.httpCookieAcceptPolicy = .always
        configuration.httpShouldSetCookies = true
        configuration.timeoutIntervalForRequest = 10
        session = URLSession(configuration: configuration)
    }

    static var configuredBaseURL: String {
        let configured = Bundle.main.object(forInfoDictionaryKey: "DEMO_BANK_BASE_URL") as? String
        return configured?.isEmpty == false ? configured! : "http://127.0.0.1:3000"
    }

    func login(username: String, password: String) async throws -> SessionUser {
        let body = ["username": username, "password": password]
        let response: LoginResponse = try await request(path: "/api/login", method: "POST", body: body)
        return response.user
    }

    func accounts() async throws -> [BankAccount] {
        let response: AccountsResponse = try await request(path: "/api/accounts")
        return response.accounts
    }

    func transactions(accountID: Int, search: String = "") async throws -> [BankTransaction] {
        var components = URLComponents()
        components.path = "/api/transactions"
        components.queryItems = [URLQueryItem(name: "account_id", value: String(accountID))]
        if !search.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            components.queryItems?.append(URLQueryItem(name: "search", value: search))
        }
        let response: TransactionsResponse = try await request(path: components.string ?? "/api/transactions")
        return response.transactions
    }

    func backendMode() async -> String {
        do {
            let response: HealthResponse = try await request(path: "/api/health")
            return response.mode ?? "connected"
        } catch {
            return "connected"
        }
    }

    func transfer(from: Int, to: Int, amount: Double, description: String) async throws -> TransferResponse {
        struct TransferRequest: Encodable {
            let fromAccountId: Int
            let toAccountId: Int
            let amount: Double
            let description: String

            enum CodingKeys: String, CodingKey {
                case fromAccountId = "from_account_id"
                case toAccountId = "to_account_id"
                case amount
                case description
            }
        }

        return try await request(
            path: "/api/transfer",
            method: "POST",
            body: TransferRequest(
                fromAccountId: from,
                toAccountId: to,
                amount: amount,
                description: description
            )
        )
    }

    private func request<Response: Decodable>(
        path: String,
        method: String = "GET"
    ) async throws -> Response {
        try await request(path: path, method: method, encodedBody: nil)
    }

    private func request<Response: Decodable, Body: Encodable>(
        path: String,
        method: String,
        body: Body
    ) async throws -> Response {
        try await request(path: path, method: method, encodedBody: try encoder.encode(body))
    }

    private func request<Response: Decodable>(
        path: String,
        method: String,
        encodedBody: Data?
    ) async throws -> Response {
        guard let url = URL(string: path, relativeTo: baseURL) else {
            throw DemoBankAPIError.invalidBaseURL
        }
        var request = URLRequest(url: url)
        request.httpMethod = method
        request.timeoutInterval = 10
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        if let encodedBody {
            request.httpBody = encodedBody
            request.setValue("application/json; charset=utf-8", forHTTPHeaderField: "Content-Type")
        }

        let (data, response) = try await session.data(for: request)
        guard let http = response as? HTTPURLResponse else {
            throw DemoBankAPIError.invalidResponse
        }
        guard (200..<300).contains(http.statusCode) else {
            let message = (try? decoder.decode(ErrorResponse.self, from: data).error)
                ?? "Request failed with status \(http.statusCode)."
            throw DemoBankAPIError.server(message: message, statusCode: http.statusCode)
        }
        return try decoder.decode(Response.self, from: data)
    }
}
