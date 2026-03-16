# ChestLogger

A client-side Fabric mod for Minecraft 1.21.4 that automatically logs **ChestShop** data and **Lands bank transactions** from chat messages directly into a Google Sheet.

---

## Google Sheet Setup

You will need **two separate Google Sheets** — one for shop logs and one for payment logs.

### Step 1 - Create the Shops Spreadsheet

1. Go to [Google Sheets](https://sheets.google.com) and create a new spreadsheet.
2. Name it something like **"ChestLogger - Shops"**.
3. Add these headers in row 1:

| A | B | C | D | E | F | G |
|---|---|---|---|---|---|---|
| Date | Chest Location | Logger | Item | Stock | Buy/Sell/Both | Shop Owner |

### Step 2 - Create the Payments Spreadsheet

1. Create another new spreadsheet.
2. Name it something like **"ChestLogger - Payments"**.
3. Add these headers in row 1:

| A | B | C | D | E |
|---|---|---|---|---|
| Date | National Treasury | Change Amount | Player | Movement |

### Step 3 - Add the Apps Script to Each Sheet

Repeat the following for **both** spreadsheets:

1. Open the spreadsheet.
2. Go to **Extensions > Apps Script**.
3. Delete any existing code in the editor.
4. Paste the following script:

**For the Shops spreadsheet:**

```javascript
var API_KEY = "CHANGE_THIS_TO_A_SECRET_KEY";

function doPost(e) {
  var payload = JSON.parse(e.postData.contents);

  if (payload.apiKey !== API_KEY) {
    return ContentService
      .createTextOutput(JSON.stringify({ error: "Unauthorized" }))
      .setMimeType(ContentService.MimeType.JSON);
  }

  var sheet = SpreadsheetApp.getActiveSpreadsheet().getActiveSheet();
  var data = payload.data;

  sheet.appendRow([
    data.date,
    data.chestLocation,
    data.logger,
    data.item,
    data.stock,
    data.buySellType,
    data.shopOwner
  ]);

  return ContentService
    .createTextOutput(JSON.stringify({ status: "ok" }))
    .setMimeType(ContentService.MimeType.JSON);
}
```

**For the Payments spreadsheet:**

```javascript
var API_KEY = "CHANGE_THIS_TO_A_SECRET_KEY";

function doPost(e) {
  var payload = JSON.parse(e.postData.contents);

  if (payload.apiKey !== API_KEY) {
    return ContentService
      .createTextOutput(JSON.stringify({ error: "Unauthorized" }))
      .setMimeType(ContentService.MimeType.JSON);
  }

  var sheet = SpreadsheetApp.getActiveSpreadsheet().getActiveSheet();
  var data = payload.data;

  sheet.appendRow([
    data.date,
    data.treasury,
    data.changeAmount,
    data.player,
    data.movement
  ]);

  return ContentService
    .createTextOutput(JSON.stringify({ status: "ok" }))
    .setMimeType(ContentService.MimeType.JSON);
}
```

5. **Change `CHANGE_THIS_TO_A_SECRET_KEY`** to a secret key. Use the **same key** in both scripts. See the section below on how to generate one.
6. Click **Save** (Ctrl+S).

### How to Get an API Key

The API key is **not** a Google API key. It is a private password that you create yourself. The mod sends it with every request, and the Apps Script checks it before writing data. This prevents anyone else from adding rows to your sheet if they find the URL.

**To generate one:**

1. Go to any password generator — for example [https://randomkeygen.com](https://randomkeygen.com)
2. Copy one of the generated keys (use one from the "Fort Knox Passwords" section for a strong key)
3. Or just make up your own — any random string works, for example: `xK9pL_myServer2026_qW3r`

**Rules:**
- Use the **same key** in both Apps Scripts and in the mod config
- Do **not** share your key with anyone who shouldn't have write access to your sheets
- If your key is ever leaked, change it in both Apps Scripts (redeploy) and in the mod config

### Step 4 - Deploy Both Scripts

Repeat the following for **both** spreadsheets:

1. Click **Deploy > New deployment** (top right).
2. Click the gear icon next to "Select type" and choose **Web app**.
3. Set the following:
   - **Description**: ChestLogger
   - **Execute as**: Me
   - **Who has access**: Anyone
4. Click **Deploy**.
5. You will be asked to authorize the script. Click **Authorize access**, choose your Google account, and allow the permissions.
6. **Copy the Web app URL** that appears. It looks like:
   ```
   https://script.google.com/macros/s/AKfycbw.../exec
   ```

You should now have **two URLs** — one for shops and one for payments.

---

## Mod Configuration

1. Open the config file located at:
   ```
   <your minecraft folder>/config/chestlogger.json
   ```
2. Fill in the URLs and API key:
   ```json
   {
     "shopSheetUrl": "https://script.google.com/macros/s/SHOPS_SCRIPT_URL/exec",
     "paymentSheetUrl": "https://script.google.com/macros/s/PAYMENTS_SCRIPT_URL/exec",
     "apiKey": "myServer_2026_xK9pL",
     "enabled": true
   }
   ```
3. Save the file.
4. Launch Minecraft.

> **Important:** The `apiKey` in the config must match the `API_KEY` in both Apps Scripts exactly. If they don't match, requests will be rejected.

---

## How It Works

### Shop Logging

When you left-click a ChestShop sign in-game, the server sends shop information in chat:

```
Shop Information:
Owner: _Mutton
Stock: 134
Item: [Spore Blossom]
Buy 1 for 400 Coins
Sell 1 for 395 Coins
```

The mod detects this, parses the data, and sends a row to the **Shops** sheet:

| Date | Chest Location | Logger | Item | Stock | Buy/Sell/Both | Shop Owner |
|---|---|---|---|---|---|---|
| 16/03/2026 | -6956, 108, 732 | YourName | Spore Blossom | 134 | Both | _Mutton |

The **Buy/Sell/Both** column indicates whether the shop offers buying, selling, or both.

### Bank Transaction Logging

When you deposit or withdraw from a Lands bank:

```
[Lands] You successfully put $30,000.00 in the bank of land Brezg.
New balance of the bank: $5,189,380.00
```

The mod parses this and sends a row to the **Payments** sheet:

| Date | National Treasury | Change Amount | Player | Movement |
|---|---|---|---|---|
| 16/03/2026 | 5189380 | 30000 | YourName | Input |

- **Input** = money deposited into the bank
- **Withdraw** = money taken out of the bank
- Change Amount is negative for withdrawals

---

## Updating a Script

If you need to update an Apps Script after the initial deployment:

1. Open the spreadsheet and go to **Extensions > Apps Script**.
2. Edit the code.
3. Click **Deploy > Manage deployments**.
4. Click the pencil icon on your existing deployment.
5. Change **Version** to **New version**.
6. Click **Deploy**.

The URL stays the same. No config change needed.

---

## Troubleshooting

### Nothing appears on the sheet
- Make sure both URLs in the config file are correct.
- Make sure the `apiKey` in the config matches the `API_KEY` in both Apps Scripts.
- Make sure both Apps Scripts are deployed as **Web apps** with access set to **Anyone**.
- Check Minecraft logs (`logs/latest.log`) for error messages from the mod.

### "Unauthorized" in logs
- The API key in `chestlogger.json` does not match the `API_KEY` variable in the Apps Script. Make sure they are identical.

### Duplicate entries
- The mod ignores identical chat messages within 2 seconds. If you're still seeing duplicates, the server may be sending slightly different messages.

### Mod not detecting shop messages
- Make sure you are **left-clicking** the ChestShop sign to view the shop info in chat.
- The mod only parses messages that start with `Shop Information`.

### Config file not appearing
- Launch Minecraft once with the mod installed, then close the game. The config file is generated on first launch.
