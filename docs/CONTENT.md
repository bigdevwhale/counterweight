# Counterweight / «Противовес»: the catalogue

The app is an old brass balance in a «Палата мер и весов». A worry is a **stone** on the left pan. The app lays brass **weights** on the right pan, one by one. Each weight carries one argument. When the weights outweigh the stone, the beam tips and the stone gets lighter.

The catalogue holds **worries**, grouped in spheres, and the **weights** for each worry. Each sphere lives in its own file, `app/src/main/assets/content/<sphere>.tsv`, where the file name is the sphere code in lower case.

## File format

- The encoding is UTF-8 with LF line endings, and fields are separated by TAB.
- Lines that start with `#` are comments. Blank lines are allowed.
- **Never put a TAB inside a text.** Never wrap a text in quotes either.

There are three kinds of lines.

```
@	KIND	ru	en	source                       a sphere-wide weight: fits any worry of this sphere (put these at the top)
W	key	ru	en	kw_ru	kw_en	flags         a worry
G	KIND	ru	en	source                       a weight for the worry above it
```

- **key** is a unique ascii snake_case id that starts with the sphere code in lower case, for example `money_mortgage_payment` or `health_mole_changed`.
- **ru / en** for a `W` line are the worry as the person would say it to themselves, in the first person:
  - «Не смогу платить ипотеку» / "I won't be able to pay the mortgage".
  - Keep it short, at most 70 characters, with no final period.
- **kw_ru / kw_en** are 4–10 search stems, comma-separated and lower case. The app matches what the person types against them, so use **word stems** that catch the inflections.
  - Russian example: `ипотек,кредит,банк,плат,долг,просроч`.
  - English example: `mortgage,loan,bank,pay,debt,arrears`.
  - Add synonyms and colloquial words people really type, such as `уволят,попрут,выгонят`.
- **flags** is `-`, or `HEAVY` when the fear can genuinely come true and be terrible. Examples are a suspected cancer, a parent's death, or losing a pregnancy. Every `HEAVY` worry must have 1–2 `TRUTH` weights.
- **source** is required for `FACT` and `VOICE` and is `-` for the other kinds:
  - For a `FACT`, the source is the organisation or study and the year, for example `ВОЗ, 2023` / `Росстат, 2024` / `Gallup, 2022`.
  - For a `VOICE`, the source is the author, and the work if known, for example `Сенека, Письма к Луцилию`.
  - Write the source once, in the language of the original or in Russian. The same string serves both languages.

### Gender

The app speaks to the person as «ты». The Russian past tense needs gender marks of the form `{male|female}`:

- `справил{ся|ась}`
- `сделал{|а}`
- `{один|одна}`

Use them only in Russian text and only where the person is the subject. Braces must balance, and there must be no nested braces.

## Kinds of weight

Each kind is a different metal on the pan. The mass is fixed in code.

| KIND | metal | what it carries |
|---|---|---|
| `FACT` | cast iron, the heaviest | A real, checkable figure or fact, with a source. |
| `AFTER` | brass | The worst case taken step by step: «и что тогда?». Steps are separated by ` → ` (space, arrow, space), 3–5 steps, ending on solid ground. Example: «Сократят → выходное пособие и время на поиск → новая работа, может, не сразу та → ты остаёшься собой, с опытом и руками». |
| `POWER` | copper | One small concrete action within the person's control, doable today or this week. |
| `BRIGHT` | silver | The other side: what good can come of it, or what this worry says about what you value. Never «всё к лучшему» about real tragedies. |
| `QUESTION` | glass | A Socratic question to ask yourself, ending with «?». |
| `VOICE` | bronze | A real, accurately attributed quote, or a close paraphrase marked «по мысли …» / "as … put it". From the Stoics, writers, scientists or psychologists. |
| `FEATHER` | a feather | Light, kind humour. It never mocks the fear. |
| `TRUTH` | dark steel | Only for `HEAVY` worries. It is honest and does not reassure falsely. Example: «Это правда может случиться, и бояться — нормально. Страх говорит о том, как сильно ты любишь». |

## How much

- Each sphere file has **about 50 worries**, and no fewer than 45.
- Each worry has **12–16 weights**. The required minimum is:
  - `FACT` ×1–3
  - `AFTER` ×2
  - `POWER` ×2
  - `BRIGHT` ×2
  - `QUESTION` ×2
  - `VOICE` ×1
  - `FEATHER` ×1
  - `TRUTH` ×1–2, only for `HEAVY` worries
- At the top of each file, put **20–30 sphere-wide `@` weights** that fit any worry of the sphere.

## Text rules

- **Be concrete.** The worries must be specific, real things that people lie awake over:
  - Good: «Не хватит денег до зарплаты», «Начальник не ответил на письмо — значит, недоволен», «Родинка изменила цвет», «Мне 45, и меня никуда не возьмут».
  - Bad: «Деньги».
  - Cover the whole sphere: different ages, situations and degrees of seriousness, from the everyday to the existential.
- **Make every weight specific to its worry.** A weight that would fit any worry belongs in the sphere-wide `@` lines or nowhere. Do not repeat the same argument with different words across worries.
- **Length.** Each weight is 40–220 characters in Russian. The English text is a natural, idiomatic version, not a word-for-word translation.
- **Tone.** Warm, clear and honest, as from a wise friend who knows the facts. Address the person as «ты» / "you".
  - No toxic positivity.
  - No guarantees («всё обязательно будет хорошо»).
  - No moralising, and no «просто не думай об этом».
- **Facts must be true.** Only use figures you are highly confident of and that are widely cited.
  - If unsure, round conservatively and hedge («больше половины», «по данным большинства исследований»), or use another kind of weight instead.
  - You may use WebSearch to check a figure you are unsure about. If you cannot verify it, drop it.
  - Do not invent studies, numbers or quotes.
  - Russian text may cite Russian facts and law, prefixed «В России …». The English text should then give the general or international version.
- **Health.** Never diagnose. A `POWER` weight may say to see a doctor or which specialist. A `FACT` weight may describe base rates, for example that most moles and lumps are benign, but only accurately.
- **Crisis.** Do not write worries about suicide or self-harm. The app routes those to a help screen.

## Example

```
W	money_short_till_payday	Не хватит денег до зарплаты	I won't have enough money to last till payday	зарплат,до получк,не хват,денег нет,конец месяц,дотян	payday,paycheck,run out,last till,end of the month,broke	-
G	POWER	Выпиши, сколько дней до зарплаты и сколько денег есть, — раздели. Конкретная сумма в день пугает меньше, чем туман	Write down how many days till payday and how much you have, then divide. A concrete daily amount is less frightening than fog	-
G	AFTER	Не хватит → займёшь у близкого или возьмёшь беспроцентный период по карте → вернёшь с зарплаты → ещё один месяц прожит	It runs short → you borrow from someone close or use a card's interest-free period → you pay it back on payday → another month lived through	-
G	QUESTION	Сколько раз до этого «не хватит» — и чем это кончилось на самом деле?	How many times before has it been "not enough", and how did it actually end?	-
G	FEATHER	Гречка, по слухам, ещё никого не подвела	Rice and beans, word has it, have never let anyone down	-
```
