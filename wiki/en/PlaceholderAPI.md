# PlaceholderAPI

If PlaceholderAPI is installed on the server, OlMeteor automatically registers the `olmeteor` expansion. No separate eCloud download is needed.

---

## Placeholder Table

### Active Meteor Info

| Placeholder | Result | Description |
|---|---|---|
| %olmeteor_active% | Number | Active meteor count |
| %olmeteor_eventid% | Text | ID of the nearest active meteor to the player |
| %olmeteor_phase% | Text | Phase of the nearest meteor (Impact, Active, etc.) |
| %olmeteor_distance% | Number | Distance to the nearest meteor in blocks |
| %olmeteor_type% | Text | Localized type name of the nearest meteor |
| %olmeteor_active_type% | Text | Same as %olmeteor_type% |
| %olmeteor_boss_alive% | true/false | Is the boss alive? |

### Timer

| Placeholder | Result | Description |
|---|---|---|
| %olmeteor_next_time% | HH:MM:ss | Time remaining until next auto meteor |

### Player Statistics

| Placeholder | Result | Description |
|---|---|---|
| %olmeteor_player_damage% | Number | Player's persistent total meteor damage |
| %olmeteor_player_kills% | Number | Player's persistent meteor kill count |
| %olmeteor_player_loot% | Number | Player's collected meteor loot count |
| %olmeteor_player_rank% | #rank | Damage rank in the nearest active meteor |

---

## Usage Examples

### Scoreboard Example:
```
Meteor: %olmeteor_active_type%
Distance: %olmeteor_distance% blocks
Your Damage: %olmeteor_player_damage%
Next meteor: %olmeteor_next_time%
```

### Hologram Example (DecentHolograms):
```
%olmeteor_active_type%
Distance: %olmeteor_distance% blocks
Status: %olmeteor_phase%
```

---

## Important Notes

| Condition | Placeholder Value |
|---|---|
| No active meteor in player's world | none, 0, false |
| No statistics recorded | 0 |
| No boss or boss defeated | false |
| Auto system disabled | --- |

---

## Troubleshooting

If placeholders are not working:

1. Check with `/olmeteor debug` that the PlaceholderAPI hook is active
2. Verify you have the correct PlaceholderAPI version
3. Do a full server restart (do not use /reload)
4. Verify the expansion is registered: /papi list
