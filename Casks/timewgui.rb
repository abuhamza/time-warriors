cask "timewgui" do
  version "1.3.1"
  sha256 "2c8fd88a3e2d0bdb70db722209d533446da840cfd5fc38d61962ee66908fe6a2"

  url "https://github.com/abuhamza/time-warriors/releases/download/v#{version}/TimewGUI-#{version}.dmg"
  name "TimewGUI"
  desc "Compose Multiplatform desktop GUI for Timewarrior"
  homepage "https://github.com/abuhamza/time-warriors"

  depends_on formula: "timewarrior"

  app "TimewGUI.app"
end
