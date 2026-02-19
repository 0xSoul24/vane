package org.oddlama.vane.portals.portal;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

public enum Orientation {
    POSITIVE_X(Plane.YZ, new Vector(1, 0, 0)),
    NEGATIVE_X(Plane.YZ, new Vector(-1, 0, 0)),
    POSITIVE_Y(Plane.XZ, new Vector(0, 1, 0)),
    NEGATIVE_Y(Plane.XZ, new Vector(0, -1, 0)),
    POSITIVE_Z(Plane.XY, new Vector(0, 0, 1)),
    NEGATIVE_Z(Plane.XY, new Vector(0, 0, -1));

    private Plane plane;
    private Vector vector;

    private Orientation(Plane plane, Vector vector) {
        this.plane = plane;
        this.vector = vector;
    }

    public Plane plane() {
        return plane;
    }

    public Vector vector() {
        return vector.clone();
    }

    public Vector componentMask() {
        return this.vector().multiply(this.vector);
    }

    public Orientation flip() {
        switch (this) {
            case NEGATIVE_X:
                return POSITIVE_X;
            case POSITIVE_X:
                return NEGATIVE_X;
            case NEGATIVE_Z:
                return POSITIVE_Z;
            case POSITIVE_Z:
                return NEGATIVE_Z;
            case NEGATIVE_Y:
                return POSITIVE_Y;
            case POSITIVE_Y:
                return NEGATIVE_Y;
        }

        // Unreachable
        throw new RuntimeException("Invalid control flow. This is a bug.");
    }

    public Location apply(
        final Orientation reference,
        final Location location,
        final boolean flipSourceIfNotOpposing
    ) {
        final var l = location.clone();
        l.setDirection(apply(reference, location.getDirection(), flipSourceIfNotOpposing));
        return l;
    }

    public Vector apply(final Orientation reference, final Vector vector, final boolean flipSourceIfNotOpposing) {
        final var x = vector.getX();
        final var y = vector.getY();
        final var z = vector.getZ();

        var effectiveSource = this;
        final var cmask = componentMask();
        final var opposing = (this.vector.dot(cmask) < 0) != (vector.dot(cmask) < 0);
        if (flipSourceIfNotOpposing && opposing) {
            effectiveSource = effectiveSource.flip();
        }

        switch (effectiveSource) {
            case NEGATIVE_X: // Looking east
                switch (reference) {
                    case NEGATIVE_X: // west
                        return new Vector(-x, y, -z);
                    case POSITIVE_X: // east
                        return new Vector(x, y, z);
                    case NEGATIVE_Z: // north
                        return new Vector(z, y, -x);
                    case POSITIVE_Z: // south
                        return new Vector(-z, y, x);
                    case NEGATIVE_Y: // down
                        return new Vector(y, -x, z);
                    case POSITIVE_Y: // up
                        return new Vector(-y, x, z);
                }
                break;
            case POSITIVE_X: // Looking west
                switch (reference) {
                    case NEGATIVE_X: // west
                        return new Vector(x, y, z);
                    case POSITIVE_X: // east
                        return new Vector(-x, y, -z);
                    case NEGATIVE_Z: // north
                        return new Vector(-z, y, x);
                    case POSITIVE_Z: // south
                        return new Vector(z, y, -x);
                    case NEGATIVE_Y: // down
                        return new Vector(-y, x, z);
                    case POSITIVE_Y: // up
                        return new Vector(y, -x, z);
                }
                break;
            case NEGATIVE_Z: // Looking south
                switch (reference) {
                    case NEGATIVE_X: // west
                        return new Vector(-z, y, x);
                    case POSITIVE_X: // east
                        return new Vector(z, y, -x);
                    case NEGATIVE_Z: // north
                        return new Vector(-x, y, -z);
                    case POSITIVE_Z: // south
                        return new Vector(x, y, z);
                    case NEGATIVE_Y: // down
                        return new Vector(x, -z, y);
                    case POSITIVE_Y: // up
                        return new Vector(-x, z, y);
                }
                break;
            case POSITIVE_Z: // Looking north
                switch (reference) {
                    case NEGATIVE_X: // west
                        return new Vector(z, y, -x);
                    case POSITIVE_X: // east
                        return new Vector(-z, y, x);
                    case NEGATIVE_Z: // north
                        return new Vector(x, y, z);
                    case POSITIVE_Z: // south
                        return new Vector(-x, y, -z);
                    case NEGATIVE_Y: // down
                        return new Vector(-x, z, y);
                    case POSITIVE_Y: // up
                        return new Vector(x, -z, y);
                }
                break;
            case NEGATIVE_Y: // Looking up
                switch (reference) {
                    case NEGATIVE_X: // west
                        return new Vector(-y, 0, 0);
                    case POSITIVE_X: // east
                        return new Vector(y, 0, 0);
                    case NEGATIVE_Z: // north
                        return new Vector(0, 0, -y);
                    case POSITIVE_Z: // south
                        return new Vector(0, 0, y);
                    case NEGATIVE_Y: // down
                        return new Vector(x, -y, z);
                    case POSITIVE_Y: // up
                        return new Vector(x, y, z);
                }
                break;
            case POSITIVE_Y: // Looking down
                switch (reference) {
                    case NEGATIVE_X: // west
                        return new Vector(y, 0, 0);
                    case POSITIVE_X: // east
                        return new Vector(-y, 0, 0);
                    case NEGATIVE_Z: // north
                        return new Vector(0, 0, y);
                    case POSITIVE_Z: // south
                        return new Vector(0, 0, -y);
                    case NEGATIVE_Y: // down
                        return new Vector(x, y, z);
                    case POSITIVE_Y: // up
                        return new Vector(-x, -y, -z);
                }
                break;
        }

        // Unreachable
        throw new RuntimeException("Invalid control flow. This is a bug.");
    }

    public static Orientation from(
        final Plane plane,
        final Block origin,
        final Block console,
        final Location entityLocation
    ) {
        switch (plane) {
            case XY: {
                final var originZ = origin.getZ() + 0.5;
                final var consoleZ = console.getZ() + 0.5;
                if (consoleZ > originZ) {
                    return NEGATIVE_Z;
                } else if (consoleZ < originZ) {
                    return POSITIVE_Z;
                } else {
                    if (entityLocation.getZ() > originZ) {
                        return NEGATIVE_Z;
                    } else {
                        return POSITIVE_Z;
                    }
                }
            }
            case YZ: {
                final var originX = origin.getX() + 0.5;
                final var consoleX = console.getX() + 0.5;
                if (consoleX > originX) {
                    return NEGATIVE_X;
                } else if (consoleX < originX) {
                    return POSITIVE_X;
                } else {
                    if (entityLocation.getX() > originX) {
                        return NEGATIVE_X;
                    } else {
                        return POSITIVE_X;
                    }
                }
            }
            case XZ: {
                final var originY = origin.getY() + 0.5;
                final var consoleY = console.getY() + 0.5;
                if (consoleY >= originY) {
                    return NEGATIVE_Y;
                } else { // if (consoleY < originY)
                    return POSITIVE_Y;
                }
            }
        }

        // Unreachable
        throw new RuntimeException("Invalid control flow. This is a bug.");
    }
}
