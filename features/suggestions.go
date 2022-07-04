package features

import (
	"github.com/bwmarrin/discordgo"
)

var (
	command *discordgo.ApplicationCommand
)

func InitSuggestions(s *discordgo.Session) {
	b := false // This is stupid, but it's the only way it works.
	command = &discordgo.ApplicationCommand{
		Type:              discordgo.ChatApplicationCommand,
		Name:              "suggestions",
		DefaultPermission: &b,
		Description:       "Manage suggestions",
	}

	s.ApplicationCommandCreate(s.State.User.ID, "", command)
	//todo: permissions

	println("Suggestions initialized")
}

func TeardownSuggestions(s *discordgo.Session) {
	println(command.ID)
	err := s.ApplicationCommandDelete(s.State.User.ID, "", command.ID)
	if err != nil {
		panic(err)
	}
	println("Suggestions torn down")

}
