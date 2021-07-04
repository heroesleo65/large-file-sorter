package org.example.progressbar;

import java.util.function.Consumer;

public interface ListOfItemsListener<T> {
  default void onAddedItemEvent(Class<? extends T> item) {
  }

  default void registerCloseEvent(Consumer<T> closeEvent) {
  }
}
