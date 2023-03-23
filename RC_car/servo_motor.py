import RPi.GPIO as GPIO
import time

class SERVO_MOTOR:
    
    def __init__(self, servo_pin):
        
        try:
            # GPIO 초기화
            GPIO.setmode(GPIO.BCM)
            GPIO.setup(servo_pin, GPIO.OUT)
            
            self.pwm = GPIO.PWM(servo_pin, 50)
            self.error = 0
            self.pwm.start(7.75)  ## center값 설정
            time.sleep(0.3)  ## 서보모터는 반드시 0.3초의 sleep이 필요
                        
        except Exception as e:
            self.error = 1
            print('SERVO MOTOR ERROR :', e)
    
    ## 조향 함수
    def steering(self, dutycycle):        
        try:
            self.pwm.ChangeDutyCycle(dutycycle)
            time.sleep(0.3)
        
        except:
            print('servo_motor.steering Error')
            