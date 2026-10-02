-- LoVisual HoldMyItems: поза руки относительно сцены (после hand_pose).
-- Пример: при смене предмета рука чуть "отъезжает" в сторону. Свои флаги ванили:
--   V:cancelEquip(true)  отключить ванильный подъём предмета (его заменяет скрипт)
--   V:cancelSwing(true)  отключить ванильный свинг
local side = rightArm and 1 or -1
if equipProgress > 0 then
  M:moveX(0.12 * equipProgress * side)
  M:rotateZ(-10 * equipProgress * side)
end
