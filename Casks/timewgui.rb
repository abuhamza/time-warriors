cask "timewgui" do
  version "1.2.0"
  sha256 "6e47f8dd32bada971c4248d9bd16409e168a2faf63409e36db8e09e4109c6481"

  url "https://github.com/abuhamza/time-warriors/releases/download/v#{version}/TimewGUI-#{version}.dmg"
  name "TimewGUI"
  desc "Compose Multiplatform desktop GUI for Timewarrior"
  homepage "https://github.com/abuhamza/time-warriors"

  depends_on formula: "timewarrior"

  app "TimewGUI.app"
end
