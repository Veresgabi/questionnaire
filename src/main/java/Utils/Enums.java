package Utils;

public class Enums {

    public enum Type {
        SCALE,
        CHOICE,
        TEXTUAL
    }

    public enum State {
        OPEN(0),
        PUBLISHED(1),
        CLOSED(2),
        RESULT_PUBLISHED(3);

        private final int value;

        private State(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    public enum Role {
        USER,
        UNION_MEMBER_USER,
        ADMIN,
        UNION_MEMBER_ADMIN
    }

    public enum Location {
        MAKO,
        MAKO_BORROWED,
        VAC,
        VAC_BORROWED
    }

    public enum ExcelUploadType {
        REGISTRATION_NUMBER,
        UNION_MEMBERSHIP_NUMBER
    }
}
