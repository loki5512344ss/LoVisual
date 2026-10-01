-- LoVisual HoldMyItems: поза самого предмета (после позы руки).
-- Глобалы те же, что в hand_pose.lua.

-- блоки в руке слегка поворачиваются при ходьбе
if I:isBlock() then
  M:rotateY(M:sin(time * 1.4) * 1.5)
end

-- зачарованные предметы мягко "дышат"
if I:isEnchanted() then
  local s = 1 + M:sin(time * 2.0) * 0.015
  M:scale(s, s, s)
end

-- еда и питьё: покачивание при использовании
if usingItem and (I:getUseAction() == 'eat' or I:getUseAction() == 'drink') then
  M:rotateZ(M:sin(useDuration * 1.1) * 3)
end
