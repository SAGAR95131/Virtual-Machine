// C++ — Example for Hypervisor VM
// Run: docker_run.bat examples\hello.cpp

#include <iostream>
#include <vector>
#include <algorithm>
#include <string>

using namespace std;

int main() {
    cout << "Hello from C++ on Linux! 🐧" << endl;
    cout << "Running inside Docker Hypervisor!" << endl << endl;

    vector<int> nums = {5, 2, 8, 1, 9, 3, 7, 4, 6};
    cout << "Original: ";
    for (int n : nums) cout << n << " ";
    cout << endl;

    sort(nums.begin(), nums.end());
    cout << "Sorted:   ";
    for (int n : nums) cout << n << " ";
    cout << endl;

    string msg = "HypervisorVM";
    reverse(msg.begin(), msg.end());
    cout << "\nReversed string: " << msg << endl;

    return 0;
}
