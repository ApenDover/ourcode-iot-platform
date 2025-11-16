package ts.andrey.iotcommon.configuration;

import com.nashkod.avro.Device;
import com.nashkod.avro.DeviceError;
import com.nashkod.avro.DeviceEvent;
import com.nashkod.avro.DeviceEventError;
import com.nashkod.avro.ErrorMeta;
import com.nashkod.avro.ErrorSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.kafka.listener.BatchListenerFailedException;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.stereotype.Service;
import ts.andrey.iotcommon.service.KafkaProducer;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
class SimpleBatchErrorHandler implements CommonErrorHandler {

    private final KafkaProducer kafkaDltProducerImpl;

    private static final String DEVICE_SERVICE = "deviceservice";
    private static final String DEVICE_COLLECT = "devicecollector";
    private static final String EVENT_COLLECTOR = "eventcollector";

    @Override
    public void handleBatch(Exception thrownException, ConsumerRecords<?, ?> data,
                            Consumer<?, ?> consumer, MessageListenerContainer container,
                            Runnable invokeListener) {

        final var rootCause = getRootCause(thrownException);
        final var rootMessage = rootCause.getMessage();
        final var rootStackTrace = getStackTraceAsString(rootCause);

        if (thrownException instanceof BatchListenerFailedException blfe) {
            ConsumerRecord<?, ?> failedRecord = blfe.getRecord();
            if (failedRecord != null) {
                try {
                    var errorMessage = createDltMessage(
                            failedRecord.value(),
                            rootMessage,
                            rootStackTrace
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
                final var errorMessages = new ArrayList<SpecificRecordBase>();

                for (ConsumerRecord<?, ?> record : data) {
                    var errorMessage = createDltMessage(
                            record.value(),
                            rootMessage,
                            rootStackTrace
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
        var errorMeta = ErrorMeta.newBuilder()
                .setErrorSource(getSystemByStackTrace(stackTrace))
                .setReason(message != null && !message.isBlank() ? message : "Unknown error")
                .setStackTrace(stackTrace)
                .build();

        long receivedAt = System.currentTimeMillis();

        if (originalMessage instanceof Device avroDevice) {
            return DeviceError.newBuilder()
                    .setFailedEvent(avroDevice)
                    .setErrorMeta(errorMeta)
                    .setReceivedAt(receivedAt)
                    .build();
        } else if (originalMessage instanceof DeviceEvent avroEvent) {
            return DeviceEventError.newBuilder()
                    .setFailedEvent(avroEvent)
                    .setErrorMeta(errorMeta)
                    .setReceivedAt(receivedAt)
                    .build();
        } else {
            throw new IllegalArgumentException("Unsupported message type: " + originalMessage.getClass());
        }
    }

    private Throwable getRootCause(Throwable throwable) {
        Throwable rootCause = throwable;
        while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
        }
        return rootCause;
    }

    private String getStackTraceAsString(Throwable throwable) {
        final var sw = new StringWriter();
        final var pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }

    private ErrorSource getSystemByStackTrace(String stackTrace) {
        if (stackTrace.contains(DEVICE_COLLECT)) {
            return ErrorSource.DEVICE_COLLECTOR;
        }
        if (stackTrace.contains(EVENT_COLLECTOR)) {
            return ErrorSource.EVENT_COLLECTOR;
        }
        if (stackTrace.contains(DEVICE_SERVICE)) {
            return ErrorSource.DEVICE_SERVICE;
        }
        return null;
    }

}
