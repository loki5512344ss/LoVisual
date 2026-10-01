-- LoVisual HoldMyItems: поза руки. Выполняется каждый кадр для каждой руки.
-- Положите свою версию в config/lovisual/holdmyitems/hand_pose.lua (перезагрузка на лету).
-- Глобалы: M (матрица), P (игрок), I (предмет), Easings, registry, mainHand, rightArm,
-- swingProgress, equipProgress, tickDelta, usingItem, useDuration, time.

global.breath = 0.0

local side = rightArm and 1 or -1
local t = time

-- лёгкое дыхание руки
M:moveY(M:sin(t * 1.6) * 0.004)
M:rotateZ(M:sin(t * 1.2) * 0.6 * side)

-- при спринте рука чуть опускается и вперёд
if P:isSprinting() then
  breath = M:lerp(breath, 1.0, 0.08)
else
  breath = M:lerp(breath, 0.0, 0.08)
end
M:moveY(-0.05 * breath)
M:rotateX(8 * breath, 0, 0, -0.5)

-- мечи: замах чуть шире, c упругим возвратом
if I:isSword() and swingProgress > 0 then
  local k = Easings:easeOutBack(swingProgress)
  M:rotateZ(-14 * k * side * (1 - swingProgress))
end
