use std::env;

use crate::command::binfile::binfile;
use poise::serenity_prelude as serenity;
use serenity::all::Ready;
use serenity::prelude::{Context, EventHandler, GatewayIntents};
use serenity::{async_trait, Client};
use tracing::{error, info};
use crate::command::clear::clear;

pub mod command;
pub struct Data {}
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
    let guild_id = serenity::GuildId::new(env::var("GUILD_ID").expect("GUILD_ID env variable should be set").parse::<u64>().expect("Guild ID is not valid"));

    let intents = GatewayIntents::GUILD_MESSAGES | GatewayIntents::MESSAGE_CONTENT;

    let framework = poise::Framework::builder()
        .options(poise::FrameworkOptions {
            commands: vec![binfile(), clear()],
            ..Default::default()
        })
        .setup(move |ctx, _ready, framework| {
            Box::pin(async move {
                poise::builtins::register_in_guild(ctx, &framework.options().commands, guild_id).await?;
                Ok(Data {})
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
