-- LoVisual HoldMyItems: анимация частей (квадов) модели предмета.
-- animator:rotateX/Y/Z(from, to, deg, px, py, pz)  вращение квадов с индексами from..to вокруг pivot (в пикселях)
-- animator:moveX/Y/Z(from, to, pixels), animator:scale(from, to, k, px, py, pz); quadCount = число квадов.
-- Пример: мечи слегка "дышат" клинком.
if I:isSword() and quadCount > 0 then
  animator:rotateZ(0, quadCount, M:sin(time * 2.0) * 1.5, 8, 0, 8)
end
