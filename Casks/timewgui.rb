cask "timewgui" do
  version "1.2.7"
  sha256 "4e7289d4e9f5581ae841e71ee4401f0bb3cfe3cbc248ad033d55e58c99a97b06"

  url "https://github.com/abuhamza/time-warriors/releases/download/v#{version}/TimewGUI-#{version}.dmg"
  name "TimewGUI"
  desc "Compose Multiplatform desktop GUI for Timewarrior"
  homepage "https://github.com/abuhamza/time-warriors"

  depends_on formula: "timewarrior"

  app "TimewGUI.app"
end
