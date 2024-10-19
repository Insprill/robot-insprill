use crate::Data;
use serenity::all::{GetMessages, MessageId};
use serenity::Error;

type Context<'a> = poise::Context<'a, Data, Error>;
#[poise::command(slash_command)]
pub async fn clear(
    ctx: Context<'_>,
    #[description = "How many messages to clear. Messages older than 14 days may not be deleted."] amount: Option<u16>,
) -> Result<(), Error> {
    let channel = ctx.channel_id();
    let mut messages: Vec<MessageId> = Vec::new();

    if amount.is_none() {
        ctx.say("Enter a valid amount!").await?;
        return Ok(());
    }

    let mut count = amount.unwrap_or(0);
    let deleted = count;

    while count > 0 {
        let fetch_count = if count <= 100 { count } else { 100 };
        let fetched_messages = channel
            .messages(ctx.http(), GetMessages::new().limit(fetch_count as u8))
            .await?;

        // Map each Message to its MessageId and extend the messages vector
        messages.extend(fetched_messages.into_iter().map(|msg| msg.id));
        count -= fetch_count;
    }

    channel.delete_messages(ctx.http(), messages).await?;
    ctx.defer_ephemeral().await?;
    ctx.say(format!("{deleted} messages deleted.")).await?;
    Ok(())
}