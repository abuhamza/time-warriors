cask "timewgui" do
  version "1.0.0"
  sha256 "966ef9176262bada7d7a64aea8c755eca61ce4b8360d0c2f3e66913ac5ed4e54"

  url "https://github.com/abuhamza/time-warriors/releases/download/v#{version}/TimewGUI-#{version}.dmg"
  name "TimewGUI"
  desc "Compose Multiplatform desktop GUI for Timewarrior"
  homepage "https://github.com/abuhamza/time-warriors"

  depends_on formula: "timewarrior"

  app "TimewGUI.app"
end
