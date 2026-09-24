package dev.loki.lovisual.util.animation;
import dev.loki.lovisual.features.module.modules.visuals.camera.ViewModel;
import dev.loki.lovisual.features.module.modules.visuals.items.held.HoldMyItems;

/**
 * Единая цепочка обработки equip-прогресса руки: сначала кривая HoldMyItems
 * (перелёт через ease-out-back на полном диапазоне 0..1), затем занижение
 * амплитуды от ViewModel. Порядок важен: кривая должна получать полный
 * диапазон, а ViewModel — масштабировать уже готовое движение, иначе
 * конечная позиция предмета уезжает.
 */
public enum EquipProgressComposer {
    ;

    public static float compose(float linear,
                                boolean curveEnabled, float curveC1, float curveIntensity,
                                boolean lowerEnabled, float lowering) {
        float progress = linear;
        if (curveEnabled) {
            progress = EaseCurves.shapeEquipProgress(progress, curveC1, curveIntensity);
        }
        if (lowerEnabled) {
            progress *= lowering;
        }
        return progress;
    }
}
