"""Outputs for the bridge: the BUSY Bar itself, or a preview in the terminal."""

from __future__ import annotations

from collections.abc import Callable
from dataclasses import dataclass
from typing import Protocol

from ..state import Card


@dataclass
class Inputs:
    """Callbacks a display calls when the person uses the Bar's controls."""

    ok: Callable[[], None]
    back: Callable[[], None]
    wheel: Callable[[int], None]
    # Called with True when the selector moves to BUSY, False when it leaves it.
    focus: Callable[[bool], None]
    # Whether focus mode is on right now.
    focused: Callable[[], bool]


class Display(Protocol):
    async def start(self, inputs: Inputs) -> None: ...

    async def show(self, card: Card, elapsed: float) -> None:
        """Show `card`, which has been on screen for `elapsed` seconds. Called ~10 times a second."""

    async def close(self) -> None: ...
