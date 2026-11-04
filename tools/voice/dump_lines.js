// Prints every line the app can speak as JSON {key: text}, read from index.html's logic block.
// Usage: node tools/voice/dump_lines.js > lines.json
const fs = require('fs');
const path = require('path');

const html = fs.readFileSync(path.join(__dirname, '../../index.html'), 'utf8');
const logic = html.split('/*LOGIC_START*/')[1].split('/*LOGIC_END*/')[0];
const lines = new Function(logic + '\nreturn voiceLines();')();
process.stdout.write(JSON.stringify(lines, null, 1));
