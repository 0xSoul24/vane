import asyncio
import importlib
import sys
import types

import pytest

from barmc.display import Inputs, terminal
from barmc.display.terminal import TerminalDisplay, read_windows_key
from barmc.icons import Library


def display_with_inputs():
    events = []
    focused = [False]
    inputs = Inputs(
        ok=lambda: events.append("ok"),
        back=lambda: events.append("back"),
        wheel=lambda delta: events.append(("wheel", delta)),
        focus=lambda busy: (events.append(("focus", busy)), focused.__setitem__(0, busy)),
        focused=lambda: focused[0],
    )
    display = TerminalDisplay(Library())
    display._inputs = inputs
    return display, events


@pytest.mark.parametrize("key, expected", [
    ("o", "ok"), ("\r", "ok"), ("\n", "ok"),
    ("x", "back"), ("\x7f", "back"), ("\x08", "back"), ("\x1b", "back"),
    ("[", ("wheel", -1)), ("\x1b[D", ("wheel", -1)),
    ("]", ("wheel", 1)), ("\x1b[C", ("wheel", 1)),
    ("b", ("focus", True)),
])
def test_keys_map_to_bar_controls(key, expected):
    display, events = display_with_inputs()
    display.press(key)
    assert events == [expected]


def test_unknown_keys_do_nothing():
    display, events = display_with_inputs()
    for key in ("q", "", "\x1b[A"):
        display.press(key)
    assert events == []


@pytest.mark.parametrize("codes, expected", [
    (["\xe0", "K"], "\x1b[D"), (["\x00", "M"], "\x1b[C"),  # arrows, both prefixes
    (["\xe0", "H"], ""),  # up arrow: no Bar control
    (["\r"], "\r"), (["\x08"], "\x08"), (["b"], "b"),
])
def test_windows_keys_read_like_unix_ones(codes, expected):
    pending = list(codes)
    assert read_windows_key(lambda: pending.pop(0)) == expected
    assert pending == []


def test_windows_key_polling_handles_everything_waiting():
    display, events = display_with_inputs()
    pending = ["o", "\xe0", "M", "b", "x"]

    async def scenario():
        task = asyncio.create_task(display.poll_keys(lambda: bool(pending), lambda: pending.pop(0)))
        await asyncio.sleep(terminal.KEY_POLL_SECONDS * 2)
        task.cancel()
        with pytest.raises(asyncio.CancelledError):
            await task

    asyncio.run(scenario())
    assert events == ["ok", ("wheel", 1), ("focus", True), "back"]


def test_imports_on_windows_without_termios(monkeypatch):
    """The module must not need termios or tty, which Windows lacks."""
    monkeypatch.setattr(sys, "platform", "win32")
    monkeypatch.setitem(sys.modules, "msvcrt", types.SimpleNamespace(kbhit=lambda: False, getwch=lambda: ""))
    monkeypatch.setitem(sys.modules, "termios", None)  # importing it now raises ImportError
    monkeypatch.setitem(sys.modules, "tty", None)
    try:
        # Any import of termios or tty would raise here.
        windows = importlib.reload(terminal)
        assert windows.msvcrt is sys.modules["msvcrt"]
    finally:
        monkeypatch.undo()
        importlib.reload(terminal)
