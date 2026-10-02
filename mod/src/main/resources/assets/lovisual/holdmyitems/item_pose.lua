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

-- искры у зачарованных предметов: Lua-частицы (координаты в системе руки, колбэк tick на каждый тик)
if I:isEnchanted() and M:sin(time * 9) > 0.96 then
  particleManager:addParticle({
    x = 0.0, y = 0.2, z = -0.3,
    vx = 0.0, vy = 0.01, vz = 0.0,
    lifetime = 14, size = 0.02, color = 0xFFB66CFF, gravity = -0.001,
    tick = function(p) p.size = p.size * 0.96 end
  })
end
