MY LEDGER - FINAL FEATURE UPDATE

Included in this source:
- Exact MY LEDGER launcher icon supplied by the owner.
- Shop setup with no demo companies/customers/transactions.
- Shop Profile can be edited later, including shop image/logo.
- Home branding and shop image support.
- Debit/Credit customer ledger.
- Partial payment support: remaining due = total debit - total credit.
- Fully paid accounts show zero balance and payment reminder is hidden.
- Tally-style inventory validation for transaction entries already present in the project.
- Payment Reminder dialog includes current Total Debit, Total Paid/Credit and Remaining Due.
- WhatsApp + PDF Report: creates latest ledger PDF and opens WhatsApp/share flow with attachment and reminder message.
- WhatsApp text reminder.
- SMS compose reminder.
- Other-app share fallback.

IMPORTANT ABOUT APP-TO-APP CUSTOMER PUSH NOTIFICATIONS:
A real notification on another customer's phone requires a customer-side My Ledger app/account,
a registered device token and a backend push service (for example Firebase Cloud Messaging or another backend).
This source intentionally does not include broken Firebase dependencies, so Codemagic builds are not blocked by
missing firebase-ai/appcheck dependencies. WhatsApp and SMS use installed apps directly.
