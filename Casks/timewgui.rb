cask "timewgui" do
  version "1.1.2"
  sha256 "ad7109d253edd0254017b043dda1fe864c7a7b1b4e28d71a18ff212d4283e821"

  url "https://github.com/abuhamza/time-warriors/releases/download/v#{version}/TimewGUI-#{version}.dmg"
  name "TimewGUI"
  desc "Compose Multiplatform desktop GUI for Timewarrior"
  homepage "https://github.com/abuhamza/time-warriors"

  depends_on formula: "timewarrior"

  app "TimewGUI.app"
end
