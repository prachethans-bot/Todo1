# வினை (Vinai) — Native Android App (Kotlin & Jetpack Compose)

வினை turns overwhelming tasks into small, clear next actions. This project is a native Android application built with **Kotlin**, **Jetpack Compose**, **Material 3**, **Room**, and native **AlarmManager** notifications, preserving the monochrome silver-on-obsidian aesthetic and full feature set.

## Included features

- **Four AI helper modes**: `New task`, `Next step`, `Too hard`, and `Time check`, backed by Supabase Edge Function `stepwise-assistant` when connected, plus an instant built-in local micro-step fallback and safety crisis check.
- **Persistent local runway & optional cloud sync**: Tasks, time estimates, actual-time feedback, and progress notes are stored locally in **Room** (`vinai_tasks.db`) and sync to **Supabase** when signed in.
- **Native Android scheduled reminders**: Custom circular **ClockPicker** dialog, live **CountdownRing** progress indicator, `AlarmManager` exact-alarm scheduling with 5-minute repeat reminders, `BOOT_COMPLETED` restoration, and an in-app ringing alarm modal with 5-minute snooze and focus launch.
- **Written task verification**: Completing a task opens the `TaskVerificationDialog`, verifying your written update against the task and micro-step via `verify-task-start` (or local relevance check when offline/unconfigured), saving the note, and sliding the task off the active list.
- **Focus sessions**: Adjustable countdown presets (10, 15, 25, 45 min), editable next-action and progress notes, periodic check-in prompts (`5`, `10`, or `15` min intervals), pause/resume, and 15-point focus qualification.
- **Points, streaks & comfort settings**: Daily streak tracking, 10/15-point completions with 20% +25 bonus chance, 5-second undo toast, and toggles for reward sounds, haptics, animations, and check-in sound.
- **Timed multi-step planner**: Generates work/break steps scaled to a 15–240 minute budget with editable step durations, live countdown, pause/skip/stop controls, and Android step notifications.
- **Accountability buddy**: Create and share buddy invite links, pause/resume/disconnect sharing, receive live cheer emojis, and open friend invite links (`com.todo.foradhd://?buddy=...` or pasted tokens).

## Configuration (Optional Cloud Backend)

Local task persistence, micro-step generation, focus timers, timed plans, points, streaks, and native alarms work out of the box without any account.

To enable Supabase authentication, cloud task sync, cloud AI verification, and buddy invitations, set `VITE_SUPABASE_URL`, `VITE_SUPABASE_ANON_KEY`, and optionally `VITE_PUBLIC_URL` in the **Secrets panel in AI Studio** (mapped via `.env` / `.env.example` into `BuildConfig`). The `supabase/` directory retains the database migrations and Edge Functions (`stepwise-assistant`, `verify-task-start`).
