cask "timewgui" do
  version "1.4.0"
  sha256 "325820a1b582c5803ab584051efac8ca4c23bf5bb00566cea5481aa3945e6856"

  url "https://github.com/abuhamza/time-warriors/releases/download/v#{version}/TimewGUI-#{version}.dmg"
  name "TimewGUI"
  desc "Compose Multiplatform desktop GUI for Timewarrior"
  homepage "https://github.com/abuhamza/time-warriors"

  depends_on formula: "timewarrior"

  app "TimewGUI.app"
end
