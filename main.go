package main

import (
	"flag"
	"github.com/Insprill/robot-insprill/features"
	"github.com/bwmarrin/discordgo"
	"log"
	"os"
	"os/signal"
	"syscall"
)

var (
	BotToken        = flag.String("token", "", "Bot access token")
	FeatureRegistry = []func(*discordgo.Session){
		features.InitSuggestions, features.TeardownSuggestions,
	}
)

func init() {
	flag.Parse()
}

func main() {
	if *BotToken == "" {
		panic("No bot token set.")
	}

	session, err := discordgo.New("Bot " + *BotToken)
	if err != nil {
		panic(err)
	}

	err = session.Open()
	if err != nil {
		panic(err)
	}

	for i := 0; i < len(FeatureRegistry); i += 2 {
		FeatureRegistry[i](session)
	}

	log.Println(session.State.User.Username + " is now running. Press CTRL+C to close.")
	sc := make(chan os.Signal, 1)
	signal.Notify(sc, syscall.SIGINT, syscall.SIGTERM, os.Interrupt, os.Kill)
	<-sc

	for i := 1; i < len(FeatureRegistry); i += 2 {
		FeatureRegistry[i](session)
	}

	err = session.Close()
	if err != nil {
		panic(err)
	}
}
