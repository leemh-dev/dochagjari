// Scope all writes to the official Seat Occupancy card; never guess unrelated controls.
const fs = require('fs');
const {chromium} = require(process.cwd() + '/app/build/pleos-browser/node_modules/playwright');
const directory = 'app/build/reports/pleos/';
(async () => {
  const result = {source: 'Official Pleos Virtual Console browser UI', toggles: [], ui_toggled: false};
  const browser = await chromium.launch({executablePath: process.env.CHROME_BIN || '/usr/bin/google-chrome', headless: true,
    args: ['--no-sandbox', '--disable-dev-shm-usage', '--use-gl=angle', '--use-angle=swiftshader', '--enable-unsafe-swiftshader']});
  const page = await browser.newPage({viewport: {width: 1440, height: 1000}});
  try {
    await page.goto('http://127.0.0.1:8088/', {waitUntil: 'domcontentloaded', timeout: 60000});
    await page.getByText('Emulation Controls', {exact: true}).first().click({timeout: 20000});
    await page.getByText('Convenience', {exact: true}).first().click({timeout: 15000});
    const title = page.getByText('Seat Occupancy', {exact: true}).first();
    await title.scrollIntoViewIfNeeded();
    const scoped = await title.evaluate(element => {
      for (let p = element.parentElement; p; p = p.parentElement) {
        if (p.innerText.length < 2000 && /Empty/.test(p.innerText) && /Occupied/.test(p.innerText)) {
          p.setAttribute('data-dochagjari-seat-card', 'true'); return true;
        }
      }
      return false;
    });
    if (!scoped) throw new Error('Seat Occupancy card could not be safely scoped');
    const card = page.locator('[data-dochagjari-seat-card="true"]');
    async function selectSeat(pattern) {
      const selects = card.locator('select');
      for (let i = 0; i < await selects.count(); i++) {
        const options = await selects.nth(i).locator('option').evaluateAll(nodes => nodes.map(n => ({text: n.textContent.trim(), value: n.value})));
        const match = options.find(o => pattern.test(o.text));
        if (match) { await selects.nth(i).selectOption(match.value); return; }
      }
      const combo = card.getByRole('combobox').first();
      if (await combo.count()) {
        await combo.click();
        const labels = await page.getByRole('option').allTextContents();
        const label = labels.find(text => pattern.test(text));
        if (label) { await page.getByRole('option', {name: label, exact: true}).click(); return; }
        await page.keyboard.press('Escape');
      }
      throw new Error('Rear seat selector not identifiable');
    }
    async function setOccupancy(value) {
      for (const role of ['radio', 'button']) {
        const target = card.getByRole(role, {name: value, exact: true});
        if (await target.count() === 1) { await target.click(); return; }
      }
      for (let i = 0; i < await card.locator('select').count(); i++) {
        const select = card.locator('select').nth(i);
        const options = await select.locator('option').evaluateAll(nodes => nodes.map(n => ({text: n.textContent.trim(), value: n.value})));
        const option = options.find(o => o.text === value);
        if (option) { await select.selectOption(option.value); return; }
      }
      const text = card.getByText(value, {exact: true});
      if (await text.count() === 1) { await text.click(); return; }
      throw new Error('Occupancy value control not identifiable');
    }
    for (const [side, pattern] of [['left', /(?:rear|row\s*2|second).*left/i], ['right', /(?:rear|row\s*2|second).*right/i]]) {
      await selectSeat(pattern);
      for (const value of ['Empty', 'Occupied', 'Empty']) {
        await setOccupancy(value); result.toggles.push({side, value, timestamp: new Date().toISOString()});
        await page.waitForTimeout(1200);
      }
    }
    result.ui_toggled = true;
  } catch (error) { result.error = error.message; }
  finally {
    result.visible_text = (await page.locator('body').innerText().catch(() => '')).slice(0, 14000);
    result.controls = await page.locator('button,select,[role="combobox"],[role="radio"],[role="option"]').evaluateAll(nodes => nodes.map(n => ({tag: n.tagName, role: n.getAttribute('role'), text: n.textContent.trim().slice(0,100)}))).catch(() => []);
    await page.screenshot({path: directory + 'virtual-console.png', fullPage: true}).catch(() => {});
    fs.writeFileSync(directory + 'console-probe.json', JSON.stringify(result, null, 2));
    await browser.close();
  }
})();
