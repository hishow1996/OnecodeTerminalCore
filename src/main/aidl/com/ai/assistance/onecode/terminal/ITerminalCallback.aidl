package com.ai.assistance.onecode.terminal;

import com.ai.assistance.onecode.terminal.CommandExecutionEvent;
import com.ai.assistance.onecode.terminal.SessionDirectoryEvent;

oneway interface ITerminalCallback {
    void onCommandExecutionUpdate(in CommandExecutionEvent event);
    void onSessionDirectoryChanged(in SessionDirectoryEvent event);
} 