use crate::Data;
use serenity::all::{GetMessages, Message};
use serenity::Error;

type Context<'a> = poise::Context<'a, Data, Error>;
#[poise::command(slash_command)]
pub async fn clear(
    ctx: Context<'_>,
    #[description = "How many messages to clear. Messages older than 14 days may not be deleted."] amount: Option<u16>,
) -> Result<(), Error> {
    let channel = ctx.channel_id();
    let mut messages: Vec<Message> = Vec::new();

    if amount.is_none() {
        ctx.say("Enter a valid amount!").await?;
        return Ok(());
    }

    let mut count = amount.unwrap_or(0);

    while count > 0 {
        if count <= 100 {
            messages.extend(channel.messages(ctx.http(), GetMessages::new().limit(count as u8)).await?);
            count -= count;
        }
        messages.extend(channel.messages(ctx.http(), GetMessages::new().limit(100)).await?);
        count -= 100;
    }
    
    channel.delete_messages(ctx.http(), messages).await?;
    Ok(())
}