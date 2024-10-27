use crate::command::binfile::binfiles;
use crate::command::clear::clear;
use crate::config::Config;
use poise::{serenity_prelude as serenity, PrefixFrameworkOptions};
use serenity::all::Ready;
use serenity::prelude::{Context, EventHandler, GatewayIntents};
use serenity::{async_trait, Client};
use std::{env, fs};
use tracing::{error, info};

pub mod command;
mod config;

pub struct Data {
    pub config: Config,
}
pub struct Handler;

#[async_trait]
impl EventHandler for Handler {
    async fn ready(&self, _: Context, ready: Ready) {
        info!("Successfully logged into {}", ready.user.tag());
    }
}

#[tokio::main]
async fn main() {
    dotenv::dotenv().expect("Failed to load .env file");

    tracing_subscriber::fmt::init();

    let token = env::var("DISCORD_TOKEN").expect("env variable `DISCORD_TOKEN` should be set");
    let guild_id = serenity::GuildId::new(
        env::var("GUILD_ID")
            .expect("GUILD_ID env variable should be set")
            .parse::<u64>()
            .expect("Guild ID is not valid"),
    );

    let intents = GatewayIntents::GUILD_MESSAGES | GatewayIntents::MESSAGE_CONTENT;

    // Load config
    let config_path = env::var("CONFIG_FILE").expect("env variable CONFIG_FILE must be set!");
    let config: Config =
        serde_yaml::from_str(&fs::read_to_string(config_path).expect("Error reading config file"))
            .expect("Error deserializing config file!");

    let framework = poise::Framework::builder()
        .options(poise::FrameworkOptions {
            commands: vec![binfiles(), clear()],
            prefix_options: PrefixFrameworkOptions {
                prefix: Some("!".into()),
                case_insensitive_commands: true,
                ..Default::default()
            },
            ..Default::default()
        })
        .setup(move |ctx, _ready, framework| {
            Box::pin(async move {
                poise::builtins::register_in_guild(ctx, &framework.options().commands, guild_id)
                    .await?;
                Ok(Data { config })
            })
        })
        .build();

    let mut client = Client::builder(&token, intents)
        .event_handler(Handler)
        .framework(framework)
        .await
        .expect("Err creating client");

    info!("Starting {}", env!("CARGO_PKG_NAME"));

    let shard_manager = client.shard_manager.clone();

    tokio::spawn(async move {
        tokio::signal::ctrl_c()
            .await
            .expect("Could not register ctrl+c handler");
        shard_manager.shutdown_all().await;
    });

    if let Err(why) = client.start().await {
        error!("Caught client error: {:?}", why);
    }
}
