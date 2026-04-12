const { onCall } = require("firebase-functions/v2/https");
const { setGlobalOptions } = require("firebase-functions/v2");
const { defineSecret } = require("firebase-functions/params");
const nodemailer = require("nodemailer");
const twilio = require("twilio");

setGlobalOptions({ maxInstances: 10 });

const GMAIL_USER = defineSecret("GMAIL_USER");
const GMAIL_PASS = defineSecret("GMAIL_PASS");
const TWILIO_SID = defineSecret("TWILIO_SID");
const TWILIO_TOKEN = defineSecret("TWILIO_TOKEN");
const TWILIO_FROM = defineSecret("TWILIO_FROM");

exports.dispatchConfirmation = onCall(
  { secrets: [GMAIL_USER, GMAIL_PASS, TWILIO_SID, TWILIO_TOKEN, TWILIO_FROM] },
  async (request) => {
    const { channel, destination, message } = request.data;

    if (!channel || !destination || !message) {
      throw new Error("Missing required fields: channel, destination, message");
    }

    if (channel === "email") {
      const transporter = nodemailer.createTransport({
        service: "gmail",
        auth: { user: GMAIL_USER.value(), pass: GMAIL_PASS.value() },
      });

      await transporter.sendMail({
        from: `"Ticket Reservation" <${GMAIL_USER.value()}>`,
        to: destination,
        subject: "Your Ticket Reservation Confirmation",
        text: message,
      });

      return { success: true, channel: "email" };
    }

    if (channel === "sms") {
      const client = twilio(TWILIO_SID.value(), TWILIO_TOKEN.value());
      await client.messages.create({
        body: message,
        from: TWILIO_FROM.value(),
        to: destination,
      });

      return { success: true, channel: "sms" };
    }

    throw new Error(`Unknown channel: ${channel}`);
  }
);
