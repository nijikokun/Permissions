package com.nijiko;

import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 *
 * @author Nijiko
 */
public class CLI {

    private LinkedHashMap<String, Object[]> commands;
    private LinkedHashMap<String, Object> arguments;
    private String message;
    private String[] message_split;
    private final String BASE = "/";
    private final String VARIABLE = "+";
    private final String COMMAND = "-";
    private final String FCSPLIT = "|";
    private final String CSPLIT = "\\|";
    private final String SPLIT = " ";
    private final String DVARIABLE = "\\:";
    private Object[] mapping;

    public CLI() {
        this.commands = new LinkedHashMap<String, Object[]>();
        this.arguments = new LinkedHashMap<String, Object>();
        this.mapping = new Object[]{};
    }

    public void add(String command, String help) {
        this.commands.put(command, new Object[]{command.split(SPLIT), help});
    }

    public void save(String message) {
        this.arguments = new LinkedHashMap<String, Object>();
        this.mapping = new Object[]{};

        this.message = message;
        this.message_split = message.split(SPLIT);
    }

    public String base() {
        if (message_split.length < 0) {
            return null;
        }

        for (String command : commands.keySet()) {
            ArrayList<Object> container = new ArrayList<Object>();
            Object[] objects = new Object[]{};
            Object[] data = commands.get(command);
            String[] command_split = (String[]) data[0];
            int location = 0;

            if (command_split.length < 0) {
                continue;
            }

            for (String section : command_split) {
                String symbol = section.substring(0, 1);
                String variable = section.substring(1, section.length());
                boolean split = variable.contains(FCSPLIT);

                if (section.startsWith(BASE)) {
                    if (split) {
                        for (String against : variable.split(CSPLIT)) {
                            if ((symbol + against).equalsIgnoreCase(message_split[location])) {
                                return variable;
                            }
                        }

                        break;
                    } else {
                        if (section.equalsIgnoreCase(message_split[location])) {
                            return variable;
                        }

                        break;
                    }
                }
            }
        }

        return null;
    }

    public String command() {
        if (message_split.length < 0) {
            return null;
        }

        for (String command : commands.keySet()) {
            ArrayList<Object> container = new ArrayList<Object>();
            Object[] objects = new Object[]{};
            Object[] data = commands.get(command);
            String[] command_split = (String[]) data[0];
            int location = 0;

            if (command_split.length < 0) {
                continue;
            }

            for (String section : command_split) {
                String symbol = section.substring(0, 1);
                String variable = section.substring(1, section.length());
                boolean split = variable.contains(FCSPLIT);

                if (symbol.equals(COMMAND)) {
                    if (message_split.length <= location) {
                        break;
                    }

                    if (split) {
                        for (String against : variable.split(CSPLIT)) {
                            if (against.equalsIgnoreCase(message_split[location]) || (symbol + against).equalsIgnoreCase(message_split[location])) {
                                return against;
                            }
                        }

                        break;
                    } else {
                        if (variable.equalsIgnoreCase(message_split[location]) || section.equalsIgnoreCase(message_split[location])) {
                            return variable;
                        }

                        break;
                    }
                }

                location++;
            }
        }

        return null;
    }

    public ArrayList<Object> parse() {
        if (message_split.length < 0) {
            return new ArrayList<Object>();
        }

        for (String command : commands.keySet()) {
            int location = 0;
            boolean foundCommand = false;
            ArrayList<Object> container = new ArrayList<Object>();
            Object[] data = commands.get(command);
            String[] command_split = (String[]) data[0];

            if (command_split.length < 0) {
                continue;
            }

            for (String section : command_split) {
                String symbol = section.substring(0, 1);
                String variable = section.substring(1, section.length());
                boolean split = variable.contains(FCSPLIT);
                String[] variables = new String[]{};
                boolean found = false;

                if (section.startsWith(BASE) || section.startsWith(COMMAND)) {
                    if (message_split.length <= location) {
                        break;
                    }

                    if (split) {
                        for (String against : variable.split(CSPLIT)) {
                            if ((section.startsWith(COMMAND) ? against : symbol + against).equalsIgnoreCase(message_split[location])
                                    || (section.startsWith(COMMAND) ? symbol + against : symbol + against).equalsIgnoreCase(message_split[location])) {
                                found = true;

                                if(section.startsWith(COMMAND)) {
                                    foundCommand = true;
                                }

                                break;
                            }
                        }
                    } else {
                        if ((section.startsWith(COMMAND) ? variable : section).equalsIgnoreCase(message_split[location])
                                || (section.startsWith(COMMAND) ? section : section).equalsIgnoreCase(message_split[location])) {
                            found = true;

                            if(section.startsWith(COMMAND)) {
                                foundCommand = true;
                            }
                        }
                    }

                    if(!found) {
                        break;
                    }
                }

                if (section.startsWith(VARIABLE)) {
                    if (message_split.length <= location) {
                        if (variable.contains(":")) {
                            variables = variable.split(DVARIABLE);

                            if (variables.length > 0) {
                                arguments.put(variables[0], variables[1]);
                            } else {
                                arguments.put(variable, 0);
                            }
                        } else {
                            arguments.put(variable, 0);
                        }
                    } else {
                        arguments.put(variable, message_split[location]);
                    }
                }

                ++location;
            }

            if (container.size() > 0) {
                return null;
            }

            if (foundCommand) {
                break;
            }
        }

        return new ArrayList(arguments.values());
    }

    public Object getValue(String argument) {
        if (this.arguments == null) {
            return null;
        }

        return (this.arguments.containsKey(argument)) ? this.arguments.get(argument) : null;
    }

    public String getString(String argument) {
        if (this.arguments == null) {
            return null;
        }

        return (this.arguments.containsKey(argument)) ? String.valueOf(this.arguments.get(argument)) : null;
    }

    public int getInteger(String argument) {
        if (this.arguments == null) {
            return 0;
        }

        int value = 0;

        try {
            value = (this.arguments.containsKey(argument)) ? Integer.valueOf(String.valueOf(this.arguments.get(argument))) : 0;
        } catch (NumberFormatException ex) {
        }

        return value;
    }

    public boolean getBoolean(String argument) {
        if (this.arguments == null) {
            return false;
        }

        return (this.arguments.containsKey(argument)) ? Boolean.parseBoolean(String.valueOf(this.arguments.get(argument))) : false;
    }

    public class InvalidSyntaxException extends Exception {

        public InvalidSyntaxException(String message) {
            super(message);
        }
    }
}
