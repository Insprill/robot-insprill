use crate::Data;
use serenity::Error;

type Context<'a> = poise::Context<'a, Data, Error>;
#[poise::command(slash_command)]
pub async fn binfile(ctx: Context<'_>) -> Result<(), Error> {
    let response = "do it work?";
    ctx.say(response).await?;
    Ok(())
}
