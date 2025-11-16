package ts.andrey.iotcommon.configuration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.kafka.listener.BatchListenerFailedException;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.stereotype.Service;
import ts.andrey.iotcommon.service.KafkaProducer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
class SimpleBatchErrorHandler implements CommonErrorHandler {

    private final KafkaProducer kafkaDltProducerImpl;

    @Override
    public void handleBatch(Exception thrownException, ConsumerRecords<?, ?> data,
                            Consumer<?, ?> consumer, MessageListenerContainer container,
                            Runnable invokeListener) {
        if (thrownException instanceof BatchListenerFailedException blfe) {
            ConsumerRecord<?, ?> failedRecord = blfe.getRecord();
            if (failedRecord != null) {
                try {
                    var errorMessage = createDltMessage(
                            failedRecord.value(),
                            blfe.getMessage(),
                            Arrays.stream(thrownException.getStackTrace())
                                    .findFirst()
                                    .toString()
                    );
                    kafkaDltProducerImpl.send(List.of(errorMessage))
                            .whenComplete((_, ex) -> {
                                if (ex != null) {
                                    log.error("Не удалось отправить в DLT: " + ex.getMessage());
                                } else {
                                    log.info("Отправлено сообщение в DLT topic");
                                }
                            });
                } catch (Exception e) {
                    log.error("Не удалось создать DLT сообщение: " + e.getMessage());
                }
            }
        } else {
            try {
                List<SpecificRecordBase> errorMessages = new ArrayList<>();

                for (ConsumerRecord<?, ?> record : data) {
                    var errorMessage = createDltMessage(
                            record.value(),
                            thrownException.getMessage(),
                            Arrays.stream(thrownException.getStackTrace())
                                    .findFirst()
                                    .toString()
                    );
                    errorMessages.add(errorMessage);
                }

                kafkaDltProducerImpl.send(errorMessages)
                        .whenComplete((_, ex) -> {
                            if (ex != null) {
                                log.error("Не удалось отправить батч в DLT: " + ex.getMessage());
                            } else {
                                log.info("Отправлено {} сообщений в DLT topic", errorMessages.size());
                            }
                        });

            } catch (Exception e) {
                log.error("Не удалось создать DLT сообщения: " + e.getMessage());
            }
        }
    }

    private SpecificRecordBase createDltMessage(Object originalMessage, String message, String stackTrace) {
        var errorMeta = com.nashkod.avro.ErrorMeta.newBuilder()
                .setErrorSource(com.nashkod.avro.ErrorSource.DEVICE_COLLECTOR)
                .setReason(StringUtils.isBlank(message) ? message : "Unknown error")
                .setStackTrace(stackTrace)
                .build();

        long receivedAt = System.currentTimeMillis();

        // В зависимости от типа оригинального сообщения создаем соответствующий DLT объект
        if (originalMessage instanceof com.nashkod.avro.Device) {
            return com.nashkod.avro.DeviceError.newBuilder()
                    .setFailedEvent((com.nashkod.avro.Device) originalMessage)
                    .setErrorMeta(errorMeta)
                    .setReceivedAt(receivedAt)
                    .build();
        } else if (originalMessage instanceof com.nashkod.avro.DeviceEvent) {
            return com.nashkod.avro.DeviceEventError.newBuilder()
                    .setFailedEvent((com.nashkod.avro.DeviceEvent) originalMessage)
                    .setErrorMeta(errorMeta)
                    .setReceivedAt(receivedAt)
                    .build();
        } else {
            throw new IllegalArgumentException("Unsupported message type: " + originalMessage.getClass());
        }
    }

}
