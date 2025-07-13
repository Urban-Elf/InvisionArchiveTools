#  This file is part of Invision Archive Tools (IAT).
#
#  Copyright (C) 2025 Mark Fisher
#
#  This program is free software: you can redistribute it and/or modify
#  it under the terms of the GNU General Public License as published by
#  the Free Software Foundation, either version 3 of the License, or
#  (at your option) any later version.
#
#  This program is distributed in the hope that it will be useful,
#  but WITHOUT ANY WARRANTY; without even the implied warranty of
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
#  GNU General Public License for more details.
#
#  You should have received a copy of the GNU General Public License
#  along with this program. If not, see https://www.gnu.org/licenses/.

from ... import shared_constants
from ... import serializable

class ButtonConfig(serializable.JSONSerializable):
    def __init__(self, text: str, shared_action: shared_constants.ButtonCallbackSA=shared_constants.ButtonCallbackSA.NONE, client_object=True):
        self.text = text
        self.shared_action = shared_action
        self.client_object = client_object # Default must be anything besides "None"

    def __serialize__(self):
        return {
            "text": self.text,
            "shared_action": self.shared_action,
            "client_object": self.client_object
        }

class ICWorkerState(serializable.JSONSerializable):
    def __init__(self, note: str, hint: str="", button_configs: list[ButtonConfig]=None, indeterminate=True):
        self.note = note
        self.hint = hint

        self.button_configs = button_configs
        self.indeterminate = indeterminate

    def is_progressive(self):
        return self.button_configs == None
    
    def __serialize__(self):
        return {
            "note": self.note,
            "hint": self.hint,
            "button_configs": self.button_configs,
            "indeterminate": self.indeterminate
        }

class SharedState:
    INITIALIZATION = ICWorkerState(note="Initializing... Please wait...")
    AUTH_REQUIRED = ICWorkerState(note="Please sign in to continue.",
                                  hint="Sign in on the browser before pressing 'Proceed'",
                                  button_configs=[ButtonConfig("Proceed")])
    VALIDATING_SESSION = ICWorkerState(note="Validating session...")
    SESSION_INVALID = ICWorkerState(note="Session invalid. Please try again.",
                                      button_configs=[ButtonConfig("OK")])
    INTERNAL_EXCEPTION = ICWorkerState(note="An internal error occurred. Open log?",
                                       hint="Please notify the developer (Help → Report Bug)",
                                       button_configs=[
                                           ButtonConfig("OK", shared_action=shared_constants.ButtonCallbackSA.OPEN_LOG),
                                           ButtonConfig("Close", shared_action=shared_constants.ButtonCallbackSA.TERMINATE)])
