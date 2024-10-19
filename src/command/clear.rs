use crate::Data;
use serenity::all::{GetMessages, MessageId};
use serenity::Error;

type Context<'a> = poise::Context<'a, Data, Error>;
#[poise::command(slash_command)]
pub async fn clear(
    ctx: Context<'_>,
    #[description = "Number of messages to clear (2-100). Messages older than 14 days may not be deleted."]
    amount: Option<u16>,
) -> Result<(), Error> {
    ctx.defer_ephemeral().await?;

    let channel = ctx.channel_id();
    let mut messages: Vec<MessageId> = Vec::new();

    if amount.is_none() {
        ctx.say("Enter a valid amount!").await?;
        return Ok(());
    }

    let mut count = amount.unwrap_or(0);
    let deleted = count;

    let fetch_count = count.min(100);
    let fetched_messages = channel
        .messages(ctx.http(), GetMessages::new().limit(fetch_count as u8))
        .await?;

    // Map each Message to its MessageId and extend the messages vector
    messages.extend(fetched_messages.into_iter().map(|msg| msg.id));

    for chunk in messages.chunks(100) {
        channel.delete_messages(ctx.http(), chunk).await?;
    }

    ctx.reply(format!("{deleted} messages deleted.")).await?;
    Ok(())
}
