package main

import (
	"flag"
	"github.com/bwmarrin/discordgo"
	"log"
	"os"
	"os/signal"
	"syscall"
)

var (
	BotToken = flag.String("token", "", "Bot access token")
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

	//todo: stuff here

	log.Println("Robot Insprill is now running. Press CTRL+C to close.")
	sc := make(chan os.Signal, 1)
	signal.Notify(sc, syscall.SIGINT, syscall.SIGTERM, os.Interrupt, os.Kill)
	<-sc

	err = session.Close()
	if err != nil {
		panic(err)
	}
}
