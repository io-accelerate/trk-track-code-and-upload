package io.accelerate.tracking.app.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.color.ForegroundCompositeConverterBase;

import static ch.qos.logback.core.pattern.color.ANSIConstants.*;

public class ColorByLevelCompositeConverter extends ForegroundCompositeConverterBase<ILoggingEvent> {

  @Override
  protected String getForegroundColorCode(ILoggingEvent event) {
    Level level = event.getLevel();
    switch (level.toInt()) {
      case Level.ERROR_INT: return BOLD+RED_FG;
      case Level.WARN_INT: return RED_FG;
      case Level.INFO_INT: return BOLD + BLACK_FG;
      default: return DEFAULT_FG;
    }

  }
}
