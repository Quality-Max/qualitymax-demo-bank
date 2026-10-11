import SwiftUI

@main
struct QualityMaxDemoBankApp: App {
    @StateObject private var model = DemoBankModel()

    var body: some Scene {
        WindowGroup {
            RootView(model: model)
                .preferredColorScheme(.dark)
        }
    }
}
