cask "timewgui" do
  version "1.2.4"
  sha256 "d8403cf5937d65f69403c2ee3f57b890279ec2765a526ab6c14be1853b2d6728"

  url "https://github.com/abuhamza/time-warriors/releases/download/v#{version}/TimewGUI-#{version}.dmg"
  name "TimewGUI"
  desc "Compose Multiplatform desktop GUI for Timewarrior"
  homepage "https://github.com/abuhamza/time-warriors"

  depends_on formula: "timewarrior"

  app "TimewGUI.app"
end
