cask "timewgui" do
  version "1.3.0"
  sha256 "ce010de0ece91cd408bf5f68e26b736c990be645940498f95d0ebd645b3f8d85"

  url "https://github.com/abuhamza/time-warriors/releases/download/v#{version}/TimewGUI-#{version}.dmg"
  name "TimewGUI"
  desc "Compose Multiplatform desktop GUI for Timewarrior"
  homepage "https://github.com/abuhamza/time-warriors"

  depends_on formula: "timewarrior"

  app "TimewGUI.app"
end
