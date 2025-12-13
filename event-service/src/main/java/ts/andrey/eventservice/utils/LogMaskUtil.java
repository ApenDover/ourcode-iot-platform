package ts.andrey.eventservice.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.experimental.UtilityClass;

import java.util.Set;

@UtilityClass
public class LogMaskUtil {

    private static final String MASK = "***";
    private static final String IGNORE_BODY_RESPONSE = "body";

    /**
     * Маскирует указанные ключи, возвращая НОВЫЙ JsonNode
     * Оригинальный JsonNode не изменяется
     */
    public JsonNode mask(JsonNode node, Set<String> keysToMask) {
        if (node == null || keysToMask == null || keysToMask.isEmpty()) {
            return node;
        }

        JsonNode copy = node.deepCopy();
        maskMutable(copy, keysToMask, true);
        return copy;
    }

    /**
     * Существующий метод для обратной совместимости
     * ВАЖНО: мутирует переданный node!
     */
    public void maskMutable(JsonNode node, Set<String> keysToMask) {
        maskMutable(node, keysToMask, true);
    }

    /**
     * Внутренний рекурсивный метод (мутирует переданный node)
     * Используется только для копий!
     */
    private void maskMutable(JsonNode node, Set<String> keysToMask, boolean skipRootBody) {
        if (node.isArray()) {
            for (JsonNode child : node) {
                maskMutable(child, keysToMask, false);
            }
        }

        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            objectNode.fields().forEachRemaining(entry -> {
                String fieldName = entry.getKey();
                JsonNode fieldValue = entry.getValue();

                if (keysToMask.contains(fieldName)
                        && !(skipRootBody && IGNORE_BODY_RESPONSE.equalsIgnoreCase(fieldName))) {
                    objectNode.set(fieldName, TextNode.valueOf(MASK));
                } else if (fieldValue != null) {
                    maskMutable(fieldValue, keysToMask, false);
                }
            });
        }
    }

}
