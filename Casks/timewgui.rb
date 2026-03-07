cask "timewgui" do
  version "1.2.6"
  sha256 "d57e5fd2e707e8416ea6551e41b14687992773b54ee1d970198032fbbd58d806"

  url "https://github.com/abuhamza/time-warriors/releases/download/v#{version}/TimewGUI-#{version}.dmg"
  name "TimewGUI"
  desc "Compose Multiplatform desktop GUI for Timewarrior"
  homepage "https://github.com/abuhamza/time-warriors"

  depends_on formula: "timewarrior"

  app "TimewGUI.app"
end
