import struct

f = open("c0610400.102200", 'rb')


class DateTime:
    def __init__(self):
        line = f.readline().strip()
        self._letter = chr(line[0])
        self._year = line[1:3].decode("utf-8")
        self._month = int(chr(line[3]), 16)
        self._day = line[4:6].decode("utf-8")
        self._hour = line[6:8].decode("utf-8")
        self._minute = line[9:11].decode("utf-8")
        self._second = line[11:13].decode("utf-8")
        self._millis = line[13:15].decode("utf-8")

    def __str__(self):
        date = "{}/{}/{} ".format(self._day, self._month, self._year)
        time = "{}:{}:{}.{}".format(self._hour, self._minute, self._second, self._millis)
        return "{} {}".format(date, time)


class Location:
    def __init__(self):
        line = f.readline().strip()
        self._location = line[0:9].strip().decode("utf-8")
        self._start_time = line[9:28].decode("utf-8")
        self._stop_time = line[29:48].decode("utf-8")
        self._higt_asl = line[49:53].decode("utf-8")
        self._longitude = line[54:60].decode("utf-8")
        self._latitude = line[61:67].decode("utf-8")
        self._zenith_angle = line[68:70].decode("utf-8")

    def __str__(self):
        return "Location: {}\nStart: {}\nEnd: {}\nAltitude: {}, longitude: {}, latitude: {}, zenith_angle: {}".format(
            self._location, self._start_time, self._stop_time, self._higt_asl, self._longitude, self._latitude,
            self._zenith_angle
        )


class LaserData:
    def __init__(self):
        line = f.readline().strip()
        self.shots_laser_1 = line[0:7].decode("utf-8")

        # XXX: Official documentation (22 february 2019) states that there should be 5 numbers but official licel app
        # only returns 4
        self.pulse_freq_1 = line[8:12].decode("utf-8")

        self.shots_laser_2 = line[13:20].decode("utf-8")

        # XXX: Official documentation (22 february 2019) states that there should be 5 numbers but official licel app
        # only returns 4
        self.pulse_freq_2 = line[21:24].decode("utf-8")

        self.datasets_num = int(line[26:28].decode("utf-8"))
        self.undocumented_laser_3 = line[29:36].decode("utf-8")
        self.undocumented_freq_3 = line[37:41].decode("utf-8")

    def __str__(self):
        return str(self.datasets_num)


class DatasetDescription:
    def __init__(self):
        line = f.readline().strip()
        self._active = bool(int(chr(line[0])))

        self._analog = False
        self._phontocounting = False
        tmp = bool(int(chr(line[2])))
        if tmp:
            self._phontocounting = True
        else:
            self._analog = True

        self._laser = int(chr(line[4]))
        self._bins = line[6:11].decode("utf-8")
        self._one = line[12]
        self._pmt_voltage = line[14:18].decode("utf-8")

        # XXX: Docs say two digits before the dot. But there is only one.
        self._binwith = line[19:23].decode("utf-8")
        self._wavelength = line[24:29].decode("utf-8")
        self._polarisation = None
        tmp = chr(line[31])
        if tmp == 'o':
            self._polarisation = "No"
        elif tmp == 's':
            self._polarisation = "Perpendicular"
        elif tmp == "i":
            self._polarisation = "parallel"
        self._adc_bits = line[43:45].decode("utf-8")
        self._number_of_shots = line[46:52].decode("utf-8")
        self._analog_range_or_disc = line[53:58].decode("utf-8")

        # XXX: According to the documentation BT = analog but in our samples from the official software BT = photon
        # we only read the TR number
        self._tr = int(chr(line[-1]))

    def __str__(self):
        print(self._tr)
        return "Active: {}, analog: {},  photoncounting: {}, " \
               "laser: {}, bins: {}".format(self._active, self._analog,
                                            self._phontocounting, self._laser,
                                            self._bins)


def read_dataset(file):
    ch = file.read(1)
    buf = []
    while True:
        if chr(ch[0]) == '\n' and chr(buf[-1]) == '\r':
            break
        buf.append(ch[0])
        ch = file.read(1)

    buf.append(ch[0])
    return bytes(buf)


class Data:
    def __init__(self):
        # \r\n
        f.readline()
        # Actual dataset, without \r\n
        line = read_dataset(f)[:-2]
        line = read_dataset(f)[:-2]
        line = read_dataset(f)[:-2]
        int_array = [x[0] for x in struct.iter_unpack('<I', line)]
        converted = [(x/58)*(500/(2**16-1)) for x in int_array]
        print(converted)


class Header:
    def __init__(self):
        self._date_time = DateTime()
        self._location = Location()
        self._laser_data = LaserData()
        self._datasets_descriptions = []
        for _ in range(self._laser_data.datasets_num):
            self._datasets_descriptions.append(DatasetDescription())

        self._data = Data()

    def __str__(self):
        print(self._laser_data)
        for i in self._datasets_descriptions:
            print(i)
        return "{}\n{}".format(self._date_time, self._location)


h = Header()

print(h)