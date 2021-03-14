#Project
The Quarkus-based back end side of the [tlvlp IoT project](https://github.com/tlvlp/iot-project-summary).

#Environment variables
| variable | description |
| --- | --- |
| MQTT_BROKER_HOST      | MQTT broker host |
| MQTT_BROKER_PORT      | MQTT broker port (default: 8883) |
| MQTT_BROKER_USER      | MQTT broker user name |
| MQTT_BROKER_PASSWORD  | MQTT broker password |

#Example MQTT payloads by topics
## /global/status
```json
{
  "id": {
    "project": "tlvlp_iot",
    "mcuName": "garden_2020"
  },
  "modules": [
    {
      "value": -1.0,
      "name": "waterTemperatureCelsius",
      "module": "ds18b20",
      "action": "read"
    },
    {
      "value": 0,
      "name": "growlight",
      "module": "relay",
      "action": "switch"
    }
  ]
  
}
```

## /global/error:
```json
{
    "id": {
      "project": "tlvlp_iot", 
      "unitName": "garden_2020"
    },
    "error": "Unable to read sensor data."
}
```

## /global/inactive:
```json
{ 
    "id": {
      "project": "tlvlp_iot", 
      "unitName": "garden_2020"
    }
}
```
