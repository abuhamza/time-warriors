cask "timewgui" do
  version "1.1.1"
  sha256 "c6e904e2b3803a0312f6e1fbbe351735c26ca3831dfd06bb2a592b2ffa5103ad"

  url "https://github.com/abuhamza/time-warriors/releases/download/v#{version}/TimewGUI-#{version}.dmg"
  name "TimewGUI"
  desc "Compose Multiplatform desktop GUI for Timewarrior"
  homepage "https://github.com/abuhamza/time-warriors"

  depends_on formula: "timewarrior"

  app "TimewGUI.app"
end
