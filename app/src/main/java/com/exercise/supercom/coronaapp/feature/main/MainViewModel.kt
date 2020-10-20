package com.exercise.supercom.coronaapp.feature.main

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_POWER
import android.os.Handler
import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import com.exercise.supercom.coronaapp.data.DatePeriodPoint
import com.exercise.supercom.coronaapp.data.model.Country
import com.exercise.supercom.coronaapp.data.model.CountryTotalCases
import com.exercise.supercom.coronaapp.feature.RemoteRepository
import kotlinx.coroutines.launch
import timber.log.Timber

const val TIME_DELAY_MS: Long = 5000
const val MAC_ADDRESS_INFECTED = "D0:03:4B:5A:E8:6E"

class MainViewModel @ViewModelInject constructor(
    private val remoteRepository: RemoteRepository,
    @Assisted private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _countries = MutableLiveData<List<Country>>()
    val countries: MutableLiveData<List<Country>>
        get() = _countries

    private val _countryTotalCases = MutableLiveData<CountryTotalCases>()
    val countryTotalCases: MutableLiveData<CountryTotalCases>
        get() = _countryTotalCases

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: MutableLiveData<Boolean>
        get() = _isLoading

    private val _userCountrySpinnerPosition = MutableLiveData<Int>()
    val userCountrySpinnerPosition: MutableLiveData<Int>
        get() = _userCountrySpinnerPosition

    private lateinit var selectedCountry: Country

    private val lexicographicalComparator by lazy {
        Comparator { country1: Country, country2: Country ->
            if (country1.country > country2.country) 1 else -1
        }
    }

    private val _fromDate = MutableLiveData<String>()
    val fromDate: MutableLiveData<String>
        get() = _fromDate

    private val _toDate = MutableLiveData<String>()
    val toDate: MutableLiveData<String>
        get() = _toDate

    private val _scanResultMessage = MutableLiveData<String>()
    val scanResultMessage: MutableLiveData<String>
        get() = _scanResultMessage

    private val _infectedMacAddress = MutableLiveData(MAC_ADDRESS_INFECTED)
    val infectedMacAddress: MutableLiveData<String>
        get() = _infectedMacAddress

    private val _isMacAddressFound = MutableLiveData<Boolean>()
    val isMacAddressFound: MutableLiveData<Boolean>
        get() = _isMacAddressFound

    private lateinit var selectedPeriodPoint: DatePeriodPoint

    private val bluetoothAdapter: BluetoothAdapter? by lazy { BluetoothAdapter.getDefaultAdapter() }

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: MutableLiveData<String>
        get() = _errorMessage

    fun fetchCountries() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val countries = remoteRepository.getAvailableCountries()

                _countries.value = countries.sortedWith(lexicographicalComparator)
                setSelectedCountryByPosition(0)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }

        }
    }

    fun fetchCountryCovid19Statistics() {
        val countryTotalCases = CountryTotalCases(country = selectedCountry.country, confirmed = 0, deaths = 0, recovered = 0)
        viewModelScope.launch {
            try {
                _isLoading.value = true
                remoteRepository.getCountryAllStatusesByPeriod(selectedCountry.slug, fromDate.value!!, toDate.value!!)
                    .onEach {
                        countryTotalCases.confirmed += it.confirmed
                        countryTotalCases.deaths += it.deaths
                        countryTotalCases.recovered += it.recovered
                    }
                _countryTotalCases.value = countryTotalCases
            } catch (e: Exception) {
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setSelectedCountryByPosition(position: Int) {
         countries.value?.let {
             selectedCountry = it[position]
        }
    }

    fun setSelectedCountryByCode(countryCode: String) {
        countries.value?.let { countries ->
            val country = countries.find { country -> country.iSO2 == countryCode }
            country?.let {
                selectedCountry = it
                _userCountrySpinnerPosition.value = countries.indexOfFirst { it.iSO2 == selectedCountry.iSO2 }
            }
        }
    }

    fun setDatePeriodPoint(datePeriodPoint: DatePeriodPoint) {
        selectedPeriodPoint = datePeriodPoint
    }

    fun setSelectedDate(year: Int, month: Int, dayOfMonth: Int) {
        when (selectedPeriodPoint) {
            DatePeriodPoint.START -> _fromDate.value = "$year-${month+1}-$dayOfMonth"
            DatePeriodPoint.END -> _toDate.value = "$year-${month+1}-$dayOfMonth"
        }
    }

    fun scanBleDevices() {
        try {
            val scanSettingsBuilder =
                ScanSettings.Builder().setScanMode(SCAN_MODE_LOW_POWER).build()
            val scanFilter =
                ScanFilter.Builder().setDeviceAddress(_infectedMacAddress.value).build()

            Handler().postDelayed({
                bluetoothAdapter?.bluetoothLeScanner?.stopScan(bleScanCallback)
            }, TIME_DELAY_MS)
            bluetoothAdapter?.bluetoothLeScanner?.startScan(
                listOf(scanFilter),
                scanSettingsBuilder,
                bleScanCallback
            )
        } catch (e: Exception) {
            Timber.e("BLE: error ${e.message}")
            isMacAddressFound.postValue(false)
            errorMessage.postValue(e.message)
        }
    }

    private var bleScanCallback: ScanCallback =
        object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                super.onScanResult(callbackType, result)
                Timber.d("BLE: MAC Address ${result?.device?.address}")
                isMacAddressFound.postValue(true)
            }

            override fun onBatchScanResults(results: List<ScanResult?>?) {
                super.onBatchScanResults(results)
                Timber.d("BLE: ${results?.toString()}")
            }

            override fun onScanFailed(errorCode: Int) {
                super.onScanFailed(errorCode)
                Timber.e("BLE: onScanFailed")
                isMacAddressFound.postValue(false)
            }
        }
}