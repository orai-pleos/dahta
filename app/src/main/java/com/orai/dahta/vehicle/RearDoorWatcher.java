package com.orai.dahta.vehicle;

import ai.pleos.playground.vehicle.Vehicle;
import ai.pleos.playground.vehicle.api.Door;
import ai.pleos.playground.vehicle.api.model.area.DoorArea;
import ai.pleos.playground.vehicle.constant.area.DoorValue;
import ai.pleos.playground.vehicle.constant.control.VehicleGear;
import ai.pleos.playground.vehicle.listener.DoorPositionListener;
import ai.pleos.playground.vehicle.listener.GearSelectionListener;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

/**
 * 뒷문이 열렸다가 닫히는 동작을 알려 준다. 손님이 탄 순간으로 본다.
 *
 * <p>왼쪽 · 오른쪽 뒷문(2열)을 각각 지켜보고, 어느 한쪽이 열렸다가 닫히면
 * {@link Listener#onRearDoorClosed()} 를 한 번 호출한다. 문 위치 값이 0보다 크면 열린 것으로 봅니다.
 *
 * <p>기어 상태도 함께 읽어, 차가 멈췄는지({@link #isStopped()}: 기어가 P 또는 N)를 알려 준다.
 */
@SuppressWarnings("deprecation") // Gear API 는 deprecated 지만 현재 기어(P/N)를 읽는 유일한 Pleos SDK 통로다.
public final class RearDoorWatcher {

  /** 뒷문 동작을 받는 쪽. */
  public interface Listener {
    /** 뒷문이 열렸다가 닫혔다. */
    void onRearDoorClosed();
  }

  private static final DoorValue[] REAR_DOORS = {DoorValue.ROW_2_LEFT, DoorValue.ROW_2_RIGHT};

  private final Listener listener;
  private final Vehicle vehicle;
  private final Door door;
  private final List<Registration> registrations = new ArrayList<>();
  /** 최근에 알려 온 기어. 아직 모르면 null. */
  private volatile VehicleGear gear;
  private final GearSelectionListener gearListener = new GearSelectionListener() {
    @Override
    public void onSelected(VehicleGear selected) {
      gear = selected;
    }

    @Override
    public void onFailed(Exception e) {
      // 기어를 못 읽으면 모르는 상태로 둔다.
    }
  };

  public RearDoorWatcher(Context context, Listener listener) {
    this.listener = listener;
    this.vehicle = new Vehicle(context.getApplicationContext());
    this.door = vehicle.getDoor();
  }

  /** 문 상태 구독을 시작한다. */
  public void start() {
    vehicle.initialize();
    for (DoorValue value : REAR_DOORS) {
      DoorArea area = new DoorArea(value);
      OpenCloseListener positionListener = new OpenCloseListener();
      door.registerDoorPosition(area, positionListener);
      registrations.add(new Registration(area, positionListener));
    }
    vehicle.getGear().registerGearSelection(gearListener);
    vehicle.getGear().getCurrentGearSelectionState(current -> {
      gear = current;
      return kotlin.Unit.INSTANCE;
    }, error -> kotlin.Unit.INSTANCE);
  }

  /**
   * 차가 멈췄는지. 기어가 P(주차) 또는 N(중립)이면 true. 기어를 아직 모르면 기능을 막지 않도록 true 로 본다.
   */
  public boolean isStopped() {
    VehicleGear current = gear;
    return current == null
        || current == VehicleGear.GEAR_UNKNOWN
        || current == VehicleGear.GEAR_PARK
        || current == VehicleGear.GEAR_NEUTRAL;
  }

  /** 구독을 끝낸다. */
  public void stop() {
    for (Registration registration : registrations) {
      door.unregisterDoorPosition(registration.area, registration.listener);
    }
    registrations.clear();
    vehicle.getGear().unregisterGearSelection(gearListener);
    vehicle.release();
  }

  private final class OpenCloseListener implements DoorPositionListener {
    private boolean open;

    @Override
    public void onPositionUpdated(Integer position) {
      boolean nowOpen = position != null && position > 0;
      if (open && !nowOpen) {
        listener.onRearDoorClosed();
      }
      open = nowOpen;
    }

    @Override
    public void onFailed(Exception e) {
      // 문 상태를 못 읽으면 동작을 알 수 없으므로 아무것도 하지 않는다.
    }
  }

  private static final class Registration {
    final DoorArea area;
    final DoorPositionListener listener;

    Registration(DoorArea area, DoorPositionListener listener) {
      this.area = area;
      this.listener = listener;
    }
  }
}
